package com.tennisplatform.student;

import com.tennisplatform.booking.AbstractBookingTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The student's photo (39-fase25.5-analisis-foto-alumno.md). */
@ExtendWith(OutputCaptureExtension.class)
class StudentPhotoApiTest extends AbstractBookingTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 5, 6, 7};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8'};

    @Test
    @SuppressWarnings("rawtypes")
    void theStudentUploadsChangesAndRemovesTheirPhotoAndTheirTeacherSeesIt(CapturedOutput output) {
        Student student = aStudentWhoMayBook();

        assertThat(upload(student, JPEG, MediaType.IMAGE_JPEG).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<byte[]> mine = photo("/api/v1/me/photo", student.token());
        assertThat(mine.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(mine.getHeaders().getCacheControl()).contains("no-store");
        assertThat(mine.getBody()).isEqualTo(JPEG);
        assertThat(photo("/api/v1/teacher/students/" + student.id() + "/photo", teacherToken).getBody()).isEqualTo(JPEG);
        assertThat(me(student)).containsEntry("hasPhoto", true);
        assertThat(detail(student)).containsEntry("hasPhoto", true);
        assertThat(list()).singleElement().satisfies(row -> assertThat(row).containsEntry("hasPhoto", true));

        upload(student, PNG, MediaType.APPLICATION_OCTET_STREAM);
        assertThat(photo("/api/v1/me/photo", student.token()).getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        upload(student, WEBP, MediaType.IMAGE_JPEG);
        assertThat(photo("/api/v1/me/photo", student.token()).getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("image/webp"));

        rest.exchange("/api/v1/me/photo", HttpMethod.DELETE, new HttpEntity<>(bearer(student.token())), Void.class);
        assertThat(photo("/api/v1/me/photo", student.token()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(me(student)).containsEntry("hasPhoto", false);
        assertThat(output.getAll()).contains("Photo of student " + student.id() + " changed");
    }

    /** By the content, not by the name or the header: a text called a JPEG is refused, and so is one too large. */
    @Test
    @SuppressWarnings("rawtypes")
    void anythingButAJpegPngOrWebpUnderOneMegabyteIsRefused() {
        Student student = aStudentWhoMayBook();

        ResponseEntity<Map> notAnImage = upload(student, "<svg onload=alert(1)>".getBytes(), MediaType.IMAGE_JPEG);
        assertThat(notAnImage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(notAnImage.getBody()).containsEntry("code", "PHOTO_INVALID");
        assertThat(upload(student, new byte[0], MediaType.IMAGE_JPEG).getBody()).containsEntry("code", "PHOTO_INVALID");

        byte[] tooLarge = Arrays.copyOf(JPEG, 1024 * 1024 + 1);
        assertThat(upload(student, tooLarge, MediaType.IMAGE_JPEG).getBody()).containsEntry("code", "PHOTO_INVALID");
        byte[] justRight = Arrays.copyOf(JPEG, 1024 * 1024);
        assertThat(upload(student, justRight, MediaType.IMAGE_JPEG).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void onlyTheTeacherWhoManagesTheStudentNowSeesIt() {
        Student student = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        upload(student, JPEG, MediaType.IMAGE_JPEG);
        String path = "/api/v1/teacher/students/" + student.id() + "/photo";

        assertThat(photo(path, other.token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        rest.exchange("/api/v1/teacher/students/" + student.id() + "/manage", HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Void.class);
        assertThat(photo(path, teacherToken).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(detail(student)).containsEntry("hasPhoto", false);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theExportCarriesThePhotoAndDeletingTheAccountDeletesIt() {
        Student student = aStudentWhoMayBook();
        upload(student, PNG, MediaType.IMAGE_PNG);

        Map export = rest.exchange("/api/v1/me/export", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class).getBody();
        assertThat((Map) export.get("photo")).containsEntry("contentType", "image/png")
                .containsEntry("base64", Base64.getEncoder().encodeToString(PNG));

        rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(student.token())), Map.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM student_photos WHERE user_id = ?", Integer.class,
                student.id())).isZero();
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> upload(Student student, byte[] image, MediaType type) {
        HttpHeaders headers = bearer(student.token());
        headers.setContentType(type);
        return rest.exchange("/api/v1/me/photo", HttpMethod.PUT, new HttpEntity<>(image, headers), Map.class);
    }

    private ResponseEntity<byte[]> photo(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(token)), byte[].class);
    }

    @SuppressWarnings("rawtypes")
    private Map me(Student student) {
        return rest.exchange("/api/v1/me", HttpMethod.GET, new HttpEntity<>(bearer(student.token())), Map.class)
                .getBody();
    }

    @SuppressWarnings("rawtypes")
    private Map detail(Student student) {
        return rest.exchange("/api/v1/teacher/students/" + student.id(), HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map> list() {
        return (List<Map>) rest.exchange("/api/v1/teacher/students", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("items");
    }
}
