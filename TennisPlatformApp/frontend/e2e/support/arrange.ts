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

  /** For a day the test fills through the screens: whatever it left there is cancelled afterwards. */
  clearAfterwards(date: LocalDate): void {
    this.undo.push(() => this.clearDay(date));
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
  async lessonAt(startsAt: Date, minutes = 30, capacity = 1, courtId?: string): Promise<string> {
    const teacher = await this.teacher();
    const lesson = ok(await teacher.POST("/teacher/lessons", {
      body: {
        type: capacity === 1 ? "INDIVIDUAL" : "GROUP",
        startsAt: startsAt.toISOString(),
        endsAt: new Date(startsAt.getTime() + minutes * 60_000).toISOString(),
        capacity,
        overrideAvailability: true,
        courtId,
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

  /** The student gives up their seat in the lesson, as they would from Mis reservas. */
  async cancelSeat(student: Student, lessonId: string): Promise<void> {
    const { client } = await signIn(student.email, student.password);
    const mine = ok(await client.GET("/bookings", { params: { query: { status: "CONFIRMED", size: 100 } } }), "bookings");
    const seat = mine.items.find((booking) => booking.lessonId === lessonId);
    if (!seat) throw new Error(`${student.email} has no seat in lesson ${lessonId}`);
    ok(await client.POST("/bookings/{id}/cancel", { params: { path: { id: seat.id } } }), "cancellation");
  }

  /** The id of the court with that name (Fase 25.6). */
  async courtId(name: string): Promise<string> {
    const teacher = await this.teacher();
    const { courts } = ok(await teacher.GET("/courts"), "courts");
    const court = courts.find((candidate) => candidate.name === name);
    if (!court) throw new Error(`There is no court called ${name}`);
    return court.id;
  }

  /** The id of the teacher's level with that name (Fase 23). */
  async levelId(name: string): Promise<string> {
    const teacher = await this.teacher();
    const { levels } = ok(await teacher.GET("/levels"), "levels");
    const level = levels.find((candidate) => candidate.name === name);
    if (!level) throw new Error(`The teacher has no level called ${name}`);
    return level.id;
  }

  /** The teacher puts the student on a level; the test leaves them without one afterwards. */
  async studentLevel(student: Student, levelId: string): Promise<void> {
    const teacher = await this.teacher();
    const path = { params: { path: { userId: student.userId } } };
    ok(await teacher.PUT("/teacher/students/{userId}/record", { ...path, body: { levelId } }), "record");
    this.undo.push(() => teacher.PUT("/teacher/students/{userId}/record", { ...path, body: { levelId: null } }));
  }

  async lessonLevel(lessonId: string, levelId: string): Promise<void> {
    const teacher = await this.teacher();
    ok(await teacher.PUT("/teacher/lessons/{id}/level", { params: { path: { id: lessonId } }, body: { levelId } }),
      "lesson level");
  }

  /** A level of the test's own, so a message to it reaches nobody else on the stack; deleted afterwards. */
  async newLevel(): Promise<{ id: string; name: string }> {
    const teacher = await this.teacher();
    const name = `E2E ${Math.random().toString(36).slice(2, 8)}`;
    const level = ok(await teacher.POST("/teacher/levels", { body: { name } }), "level");
    this.undo.push(() => teacher.DELETE("/teacher/levels/{id}", { params: { path: { id: level.id } } }));
    return { id: level.id, name };
  }

  /**
   * A pass type of the test's own (Fase 27). Afterwards the passes recorded with it are voided,
   * which the test leaves possible by giving its lessons back, and the type is deleted. One that
   * somebody bought in the app (Fase 27.5) keeps its purchase and cannot be deleted: it is retired.
   */
  async passType(lessons: number): Promise<{ id: string; name: string }> {
    const teacher = await this.teacher();
    const name = `E2E ${Math.random().toString(36).slice(2, 8)}`;
    const type = ok(await teacher.POST("/teacher/pass-types", {
      body: { name, lessonType: "GROUP", lessons, priceCents: 3000, validityMonths: 3 },
    }), "pass type");
    this.undo.push(async () => {
      const passes = ok(await teacher.GET("/teacher/passes", { params: { query: {} } }), "passes");
      for (const pass of passes.items.filter((candidate) => candidate.passTypeId === type.id)) {
        await teacher.POST("/teacher/passes/{id}/void", { params: { path: { id: pass.id } } });
      }
      const deleted = await teacher.DELETE("/teacher/pass-types/{id}", { params: { path: { id: type.id } } });
      if (deleted.error) {
        await teacher.PATCH("/teacher/pass-types/{id}", { params: { path: { id: type.id } }, body: { retired: true } });
      }
    });
    return { id: type.id, name };
  }

  /** Selling passes in the app (Fase 27.5), switched off again afterwards. */
  async sellPassesOnline(): Promise<void> {
    const teacher = await this.teacher();
    ok(await teacher.PUT("/teacher/pass-settings", { body: { onlinePayment: true } }), "pass settings");
    this.undo.push(() => teacher.PUT("/teacher/pass-settings", { body: { onlinePayment: false } }));
  }

  /** "No booking without a pass", switched off again afterwards: every other test books without one. */
  async requirePasses(): Promise<void> {
    const teacher = await this.teacher();
    ok(await teacher.PUT("/teacher/pass-settings", { body: { required: true } }), "pass settings");
    this.undo.push(() => teacher.PUT("/teacher/pass-settings", { body: { required: false } }));
  }

  /** The test sends a message through the screens; afterwards it is taken off every board (Fase 24). */
  deleteMessageAfterwards(subject: string): void {
    this.undo.push(async () => {
      const teacher = await this.teacher();
      const sent = ok(await teacher.GET("/teacher/messages", { params: { query: { size: 100 } } }), "messages");
      for (const message of sent.items.filter((candidate) => candidate.subject === subject)) {
        await teacher.DELETE("/teacher/messages/{id}", { params: { path: { id: message.id } } });
      }
    });
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
