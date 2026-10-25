#!/usr/bin/env python3
"""Fills a local stack with example data, so every screen has something to show.

Students with their level and record, lessons of each level (single ones and weekly series),
bookings, a full lesson with a waiting list, and last week's lessons with attendance. Everything
goes through the API except what the API refuses on purpose: verifying the students' addresses
(that takes an email) and lessons in the past (a lesson cannot be created once it has started),
which go straight into the database of the local container.

    python TennisPlatformApp/demo/load-demo.py --env TennisPlatformApp/.env

On a server that sends real email (the Pi, to show the app before it opens), --server keeps every
email from going out: the students are written straight into the database, with the password hash
of the local stack's example students, and so are their bookings. Load the local stack first.

    python TennisPlatformApp/demo/load-demo.py --server daniel@tenis-pi \
        --env /srv/tennis-platform/TennisPlatformApp/.env --db-container tennis-prod-postgres-1 \
        --api https://tenis-pi.tail26506d.ts.net/api/v1

The students' password is given with --student-password and kept out of the repository.
remove-demo.sql undoes it.
"""
import argparse
import datetime as dt
import json
import subprocess
import sys
import urllib.error
import urllib.request

EMAIL_DOMAIN = "demo.oasis.local"
NOTE = "Ejemplo"

STUDENTS = [
    # key, name, phone, level, goals, private notes
    ("lucia", "Lucía Ferrer", "600111222", "Iniciación", "Coger confianza con el revés", "Muy constante, siempre puntual"),
    ("carlos", "Carlos Vega", "600222333", None, None, "Acaba de empezar: pendiente de ponerle nivel"),
    ("pablo", "Pablo Ruiz", "600333444", "Intermedio", "Mejorar el saque liftado", "Le molesta el hombro si carga mucho"),
    ("marta", "Marta Gil", "600444555", "Intermedio", "Subir a la red con seguridad", None),
    ("ines", "Inés Martín", "600555666", "Avanzado", "Preparar el torneo social de diciembre", "Prefiere clases de mañana"),
    ("diego", "Diego Navas", "600666777", "Competición", "Táctica de partido", "Juega federado"),
]


def madrid():
    try:
        from zoneinfo import ZoneInfo
        return ZoneInfo("Europe/Madrid")
    except Exception:  # Windows without tzdata: the computer's own zone, which is Madrid's
        return None


ZONE = madrid()


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, expect=(200, 201, 202, 204)):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        request = urllib.request.Request(self.base + path, method=method, headers=headers,
                                         data=json.dumps(body).encode() if body is not None else None)
        try:
            with urllib.request.urlopen(request) as response:
                text = response.read()
                status, data = response.status, (json.loads(text) if text else None)
        except urllib.error.HTTPError as error:
            text = error.read()
            status, data = error.code, (json.loads(text) if text else None)
        if expect and status not in expect:
            sys.exit(f"{method} {path} answered {status}: {data}")
        return data

    def login(self, email, password):
        return self.call("POST", "/auth/login", {"email": email, "password": password})["accessToken"]


def sql(container, user, database, statement, server=None):
    command = ["docker", "exec", "-i", container, "psql", "-U", user, "-d", database, "-At", "-v", "ON_ERROR_STOP=1"]
    if server:
        command = ["ssh", "-o", "BatchMode=yes", server, " ".join(command)]
    result = subprocess.run(command, input=statement, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        sys.exit("psql failed: " + result.stderr)
    return result.stdout.strip()


def at(day, hour, minute=0):
    """The wall-clock time in Madrid, with the summer or winter offset of that day."""
    local = dt.datetime.combine(day, dt.time(hour, minute))
    return local.replace(tzinfo=ZONE) if ZONE else local.astimezone()


def lesson_body(start, minutes, capacity, level_id, note):
    body = {"type": "INDIVIDUAL" if capacity == 1 else "GROUP", "capacity": capacity,
            "startsAt": start.isoformat(), "endsAt": (start + dt.timedelta(minutes=minutes)).isoformat(),
            "notes": f"{NOTE}: {note}", "overrideAvailability": True}
    if level_id:
        body["levelId"] = level_id
    return body


def next_weekday(today, weekday):
    """The first date after today that falls on weekday (0 = Monday)."""
    return today + dt.timedelta(days=(weekday - today.weekday() - 1) % 7 + 1)


def read_env(path, server=None):
    """The stack's .env; on a server it is read over ssh and never printed."""
    if server:
        text = subprocess.run(["ssh", "-o", "BatchMode=yes", server, f"cat '{path}'"], capture_output=True, text=True,
                              encoding="utf-8", check=True).stdout
    else:
        with open(path, encoding="utf-8") as env:
            text = env.read()
    values = {}
    for line in text.splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--env", required=True, help="the stack's .env, with TEACHER_EMAIL and TEACHER_PASSWORD")
    parser.add_argument("--api", default="http://localhost:8081/api/v1")
    parser.add_argument("--db-container", default="tennis-postgres")
    parser.add_argument("--student-password", required=True,
                        help="the example students' password; on a server it must be the local stack's")
    parser.add_argument("--server", help="ssh target of a server that sends real email; see the docstring")
    parser.add_argument("--local-db-container", default="tennis-postgres",
                        help="with --server, where the example students' password hash is read from")
    args = parser.parse_args()

    env = read_env(args.env, args.server)
    db_user = env.get("DB_USER", "tennis_platform")
    db_name = env.get("DB_NAME", "tennis_platform")

    def run(statement):
        return sql(args.db_container, db_user, db_name, statement, args.server)

    api = Api(args.api)
    teacher = api.login(env["TEACHER_EMAIL"], env["TEACHER_PASSWORD"])
    teacher_id = api.call("GET", "/me", token=teacher)["id"]
    if run(f"SELECT count(*) FROM users WHERE email LIKE '%@{EMAIL_DOMAIN}'") != "0":
        sys.exit("The example data is already loaded: run remove-demo.sql first to load it again.")

    levels = {level["name"]: level["id"] for level in api.call("GET", "/levels", token=teacher)["levels"]}
    missing = [name for _, _, _, name, _, _ in STUDENTS if name and name not in levels]
    if missing:
        sys.exit(f"The teacher has no level called {missing[0]}: the example data uses the four initial ones.")

    # Students: registered, verified, with a profile, managed, and with their record.
    students = {}
    if args.server:
        # No registration: it would send a verification email to an address that does not exist.
        password_hash = sql(args.local_db_container, "tennis_platform", "tennis_platform",
                            f"SELECT password_hash FROM users WHERE email = 'demo-lucia@{EMAIL_DOMAIN}'")
        if not password_hash:
            sys.exit("Load the example data into the local stack first: its password hash is reused here.")
        for key, name, phone, *_ in STUDENTS:
            student_id = run(
                f"WITH u AS (INSERT INTO users (email, password_hash, role, status, email_verified_at) VALUES "
                f"('demo-{key}@{EMAIL_DOMAIN}', '{password_hash}', 'STUDENT', 'ACTIVE', now()) RETURNING id) "
                f"INSERT INTO student_profiles (user_id, full_name, phone) SELECT id, '{name}', '{phone}' FROM u "
                f"RETURNING user_id;").splitlines()[0]
            students[key] = {"id": student_id, "token": None}
    else:
        for key, *_ in STUDENTS:
            api.call("POST", "/auth/register", {"email": f"demo-{key}@{EMAIL_DOMAIN}", "password": args.student_password})
        run(f"UPDATE users SET email_verified_at = now(), status = 'ACTIVE' WHERE email LIKE '%@{EMAIL_DOMAIN}';")
        for key, name, phone, *_ in STUDENTS:
            token = api.login(f"demo-{key}@{EMAIL_DOMAIN}", args.student_password)
            student_id = api.call("PATCH", "/me", {"fullName": name, "phone": phone}, token=token)["id"]
            students[key] = {"id": student_id, "token": token}
    for key, name, phone, level, goals, notes in STUDENTS:
        student_id = students[key]["id"]
        api.call("POST", f"/teacher/students/{student_id}/manage", token=teacher)
        api.call("PUT", f"/teacher/students/{student_id}/record",
                 {"levelId": levels.get(level), "goals": goals, "privateNotes": notes}, token=teacher)

    today = dt.datetime.now(ZONE).date() if ZONE else dt.date.today()

    def create(start, minutes, capacity, level, note):
        return api.call("POST", "/teacher/lessons", lesson_body(start, minutes, capacity, levels.get(level), note),
                        token=teacher)["id"]

    def series(start, weeks, capacity, level, note):
        body = lesson_body(start, 60, capacity, levels.get(level), note)
        body["weeks"] = weeks
        dates = [o["startsAt"] for o in api.call("POST", "/teacher/lessons/series/preview", body, token=teacher)["occurrences"]
                 if o["status"] in ("OK", "OUTSIDE_AVAILABILITY")]
        body["dates"] = dates
        return [lesson["id"] for lesson in api.call("POST", "/teacher/lessons/series", body, token=teacher)["lessons"]]

    def book(key, lesson_id):
        if args.server:
            # Straight into the table: through the API the student and the teacher would get an email.
            run(f"INSERT INTO bookings (lesson_id, teacher_user_id, student_user_id, lesson_starts_at, lesson_ends_at) "
                f"SELECT id, teacher_user_id, '{students[key]['id']}', starts_at, ends_at FROM lessons "
                f"WHERE id = '{lesson_id}';")
        else:
            api.call("POST", f"/lessons/{lesson_id}/bookings", token=students[key]["token"])

    def wait(key, lesson_id):
        if args.server:
            run(f"INSERT INTO waiting_list_entries (lesson_id, teacher_user_id, student_user_id, joined_at) "
                f"VALUES ('{lesson_id}', '{teacher_id}', '{students[key]['id']}', now());")
        else:
            api.call("POST", f"/lessons/{lesson_id}/waiting-list", token=students[key]["token"])

    # Weekly groups of a whole term, one per level that has students.
    beginners = series(at(next_weekday(today, 1), 18), 12, 4, "Iniciación", "grupo fijo de iniciación")
    intermediate = series(at(next_weekday(today, 3), 19), 12, 4, "Intermedio", "grupo de intermedio")
    competition = series(at(next_weekday(today, 5), 10), 8, 4, "Competición", "entrenamiento de competición")
    for key, lesson in (("lucia", beginners[0]), ("carlos", beginners[0]), ("lucia", beginners[1]),
                        ("pablo", intermediate[0]), ("marta", intermediate[0]), ("diego", competition[0])):
        book(key, lesson)

    # Single lessons: an individual one for each level, and open ones anybody may book.
    individual = create(at(next_weekday(today, 2), 10), 60, 1, "Avanzado", "clase particular")
    book("ines", individual)
    create(at(next_weekday(today, 0), 9), 60, 1, None, "hueco libre para una particular")
    create(at(next_weekday(today, 4), 17, 30), 90, 6, None, "clase abierta de los viernes")
    full = create(at(next_weekday(today, 3), 10), 60, 3, None, "clase abierta de dobles")
    for key in ("lucia", "pablo", "carlos"):
        book(key, full)
    wait("marta", full)
    wait("ines", full)

    # Last week, with attendance: lessons in the past cannot be created through the API.
    last_tuesday = next_weekday(today, 1) - dt.timedelta(days=7)
    last_wednesday = next_weekday(today, 2) - dt.timedelta(days=7)
    if last_tuesday >= today:
        last_tuesday -= dt.timedelta(days=7)
    if last_wednesday >= today:
        last_wednesday -= dt.timedelta(days=7)
    past = [(at(last_tuesday, 18), 4, "Iniciación", [("lucia", "ATTENDED"), ("carlos", "NO_SHOW")]),
            (at(last_wednesday, 10), 1, "Avanzado", [("ines", "ATTENDED")])]
    for start, capacity, level, seats in past:
        end = start + dt.timedelta(hours=1)
        kind = "INDIVIDUAL" if capacity == 1 else "GROUP"
        statement = (f"WITH lesson AS (INSERT INTO lessons (teacher_user_id, type, starts_at, ends_at, capacity, notes, "
                     f"created_outside_availability, level_id) VALUES ('{teacher_id}', '{kind}', '{start.isoformat()}', "
                     f"'{end.isoformat()}', {capacity}, '{NOTE}: clase de la semana pasada', true, '{levels[level]}') "
                     f"RETURNING id, starts_at, ends_at) "
                     f"INSERT INTO bookings (lesson_id, teacher_user_id, student_user_id, attendance, lesson_starts_at, "
                     f"lesson_ends_at, booked_at) SELECT lesson.id, '{teacher_id}', s.student, s.attendance, "
                     f"lesson.starts_at, lesson.ends_at, lesson.starts_at - interval '2 days' FROM lesson, (VALUES "
                     + ", ".join(f"('{students[key]['id']}'::uuid, '{attendance}')" for key, attendance in seats)
                     + ") AS s(student, attendance);")
        run(statement)

    print(f"Example data loaded: {len(STUDENTS)} students, three weekly groups, single lessons, a waiting list "
          f"and last week's attendance. Students sign in as demo-<name>@{EMAIL_DOMAIN}, see demo/README.md.")


if __name__ == "__main__":
    main()
