import createClient from "openapi-fetch";
import type { paths } from "@/shared/api/schema";
import { addDays, dateIn, todayIn, zonedToInstant, type LocalDate } from "@/shared/time";
import { linkSentTo } from "./mailbox";
import { ADMIN, BACKEND, STUDENT_PASSWORD, TEACHER, ZONE, uniqueEmail } from "./stack";

type Client = ReturnType<typeof createClient<paths>>;

export interface Student {
  email: string;
  password: string;
  userId: string;
  fullName: string;
}

/** Fails loudly: a setup step that did not happen would make the test fail somewhere misleading. */
function ok<T>(result: { data?: T; error?: unknown; response: Response }, what: string): T {
  if (!result.response.ok) {
    throw new Error(`${what} answered ${result.response.status}: ${JSON.stringify(result.error)}`);
  }
  return result.data as T;
}

async function signIn(email: string, password: string): Promise<{ client: Client; userId: string }> {
  const login = ok(await createClient<paths>({ baseUrl: BACKEND }).POST("/auth/login", { body: { email, password } }),
    `login of ${email}`);
  return {
    client: createClient<paths>({ baseUrl: BACKEND, headers: { Authorization: `Bearer ${login.accessToken}` } }),
    userId: login.user.id,
  };
}

/**
 * What a test needs to exist before it starts, made through the API so the test only drives
 * the screens it is about. Everything it makes is undone afterwards, so the suite can also run
 * against a development stack without leaving students or lessons behind.
 */
export class Arrange {
  private readonly undo: (() => Promise<unknown>)[] = [];
  private teacherClient?: Client;

  async teacher(): Promise<Client> {
    this.teacherClient ??= (await signIn(TEACHER.email, TEACHER.password)).client;
    return this.teacherClient;
  }

  async student({ verified = true, profile = true, managed = false } = {}): Promise<Student> {
    const email = uniqueEmail();
    ok(await createClient<paths>({ baseUrl: BACKEND }).POST("/auth/register",
      { body: { email, password: STUDENT_PASSWORD } }), "register");
    if (verified) {
      const token = new URL(await linkSentTo(email, "/verify-email"), "http://any").searchParams.get("token")!;
      ok(await createClient<paths>({ baseUrl: BACKEND }).POST("/auth/verify-email", { body: { token } }), "verify");
    }
    const { client, userId } = await signIn(email, STUDENT_PASSWORD);
    const fullName = `Alumno ${email.slice(4, 17)}`;
    if (profile) {
      ok(await client.PATCH("/me", { body: { fullName } }), "profile");
    }
    const student = { email, password: STUDENT_PASSWORD, userId, fullName };
    this.undo.push(() => this.stopManaging(student));
    if (managed) {
      await this.manage(student);
    }
    return student;
  }

  async manage(student: Student): Promise<void> {
    const teacher = await this.teacher();
    ok(await teacher.POST("/teacher/students/{userId}/manage", { params: { path: { userId: student.userId } } }),
      "manage");
  }

  /**
   * A day at least two days ahead, so every booking is outside the 24-hour window, with no
   * lesson and no exception of anyone's: whatever the test puts there is its own.
   */
  async freeDay(): Promise<LocalDate> {
    const teacher = await this.teacher();
    const from = addDays(todayIn(ZONE), 2);
    const to = addDays(from, 60);
    const calendar = ok(await teacher.GET("/calendar", { params: { query: { from, to } } }), "calendar");
    const availability = ok(await teacher.GET("/teacher/availability", { params: { query: { from, to } } }),
      "availability");
    const taken = new Set([
      // Cancelled ones too: they stay on the calendar and would share the screen with the test's.
      ...calendar.lessons.map((lesson) => dateIn(lesson.startsAt, ZONE)),
      ...availability.exceptions.map((exception) => exception.date),
    ]);
    for (let day = from; day <= to; day = addDays(day, 1)) {
      if (!taken.has(day)) {
        this.undo.push(() => this.clearDay(day));
        return day;
      }
    }
    throw new Error("No free day in the next two months");
  }

  async extraHours(date: LocalDate, startTime: string, endTime: string): Promise<void> {
    const teacher = await this.teacher();
    ok(await teacher.POST("/teacher/availability/exceptions", { body: { date, type: "EXTRA", startTime, endTime } }),
      "extra hours");
  }

  async lesson({ date, time, minutes = 60, capacity = 1 }: { date: LocalDate; time: string; minutes?: number; capacity?: number }) {
    return this.lessonAt(new Date(zonedToInstant(date, time, ZONE)), minutes, capacity);
  }

  /** Outside the hours on purpose when the time is not a free day's: the test is not about availability. */
  async lessonAt(startsAt: Date, minutes = 30, capacity = 1): Promise<string> {
    const teacher = await this.teacher();
    const lesson = ok(await teacher.POST("/teacher/lessons", {
      body: {
        type: capacity === 1 ? "INDIVIDUAL" : "GROUP",
        startsAt: startsAt.toISOString(),
        endsAt: new Date(startsAt.getTime() + minutes * 60_000).toISOString(),
        capacity,
        overrideAvailability: true,
      },
    }), "lesson");
    this.undo.push(async () => teacher.POST("/teacher/lessons/{id}/cancel", { params: { path: { id: lesson.id } } }));
    return lesson.id;
  }

  /** How many students the teacher manages now, the number the student limit is compared with. */
  async managedCount(): Promise<number> {
    const teacher = await this.teacher();
    const students = ok(await teacher.GET("/teacher/students", { params: { query: { size: 100 } } }), "students");
    return students.items.filter((student) => student.managedStatus === "MANAGED").length;
  }

  /** The test changes the platform configuration through the screens; this puts it back. */
  async keepConfiguration(): Promise<void> {
    const { client } = await signIn(ADMIN.email, ADMIN.password);
    const { studentLimit, maxGroupCapacity } = ok(await client.GET("/admin/configuration"), "configuration");
    this.undo.push(() => client.PATCH("/admin/configuration", { body: { studentLimit, maxGroupCapacity } }));
  }

  async book(student: Student, lessonId: string): Promise<void> {
    const { client } = await signIn(student.email, student.password);
    ok(await client.POST("/lessons/{id}/bookings", { params: { path: { id: lessonId } } }), "booking");
  }

  /** Seats taken in a lesson, read as the teacher sees them. */
  async confirmedBookingsOf(lessonId: string): Promise<number> {
    const teacher = await this.teacher();
    const bookings = ok(await teacher.GET("/bookings", { params: { query: { lessonId, status: "CONFIRMED" } } }),
      "bookings");
    return bookings.totalItems;
  }

  /** Runs whatever the test left behind, newest first; a step that no longer applies is fine. */
  async cleanUp(): Promise<void> {
    for (const step of this.undo.reverse()) {
      await step().catch(() => undefined);
    }
  }

  private async stopManaging(student: Student) {
    const teacher = await this.teacher();
    return teacher.DELETE("/teacher/students/{userId}/manage", { params: { path: { userId: student.userId } } });
  }

  /** The lessons and exceptions a test created on its free day through the screens. */
  private async clearDay(date: LocalDate) {
    const teacher = await this.teacher();
    const calendar = ok(await teacher.GET("/calendar", { params: { query: { from: date, to: date } } }), "calendar");
    for (const lesson of calendar.lessons.filter((l) => l.status === "OPEN" || l.status === "FULL")) {
      await teacher.POST("/teacher/lessons/{id}/cancel", { params: { path: { id: lesson.id } } });
    }
    const availability = ok(await teacher.GET("/teacher/availability", { params: { query: { from: date, to: date } } }),
      "availability");
    for (const exception of availability.exceptions) {
      await teacher.DELETE("/teacher/availability/exceptions/{id}", { params: { path: { id: exception.id } } });
    }
  }
}
