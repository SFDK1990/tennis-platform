#!/usr/bin/env python3
"""Gives each example student a photo (Fase 25.5), from a folder kept out of the repository.

The files are taken in name order and given to the students in the order of load-demo.py: Lucía,
Carlos, Pablo, Marta, Inés and Diego. They go straight into the table, as the student would leave
them, so the stack must already have the example students. remove-demo.sql takes them away.

    python TennisPlatformApp/demo/load-demo-photos.py --folder <carpeta> --env TennisPlatformApp/.env
    python TennisPlatformApp/demo/load-demo-photos.py --folder <carpeta> --server daniel@tenis-pi \\
        --env /srv/tennis-platform/TennisPlatformApp/.env --db-container tennis-prod-postgres-1
"""
import argparse
import importlib.util
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("load_demo", HERE / "load-demo.py")
demo = importlib.util.module_from_spec(spec)
spec.loader.exec_module(demo)

MAX_BYTES = 1024 * 1024


def content_type(data):
    """The same reading of the first bytes as the backend: a photo is what it is, not what it is called."""
    if data[:3] == b"\xff\xd8\xff":
        return "image/jpeg"
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return "image/png"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--folder", required=True, help="one image per example student, taken in name order")
    parser.add_argument("--env", required=True, help="the stack's .env")
    parser.add_argument("--db-container", default="tennis-postgres")
    parser.add_argument("--server", help="ssh target of the server, as in load-demo.py")
    args = parser.parse_args()

    files = sorted(path for path in pathlib.Path(args.folder).iterdir() if path.is_file())
    keys = [key for key, *_ in demo.STUDENTS]
    if len(files) < len(keys):
        sys.exit(f"The folder has {len(files)} files and there are {len(keys)} example students.")

    env = demo.read_env(args.env, args.server)
    db_user = env.get("DB_USER", "tennis_platform")
    db_name = env.get("DB_NAME", "tennis_platform")

    for key, path in zip(keys, files):
        data = path.read_bytes()
        kind = content_type(data)
        if kind is None or len(data) > MAX_BYTES:
            sys.exit(f"{path.name} is not a JPEG, PNG or WebP image under 1 MB.")
        demo.sql(args.db_container, db_user, db_name,
                 f"INSERT INTO student_photos (user_id, content_type, data, updated_at) "
                 f"SELECT id, '{kind}', decode('{data.hex()}', 'hex'), now() FROM users "
                 f"WHERE email = 'demo-{key}@{demo.EMAIL_DOMAIN}' "
                 f"ON CONFLICT (user_id) DO UPDATE SET content_type = excluded.content_type, data = excluded.data, "
                 f"updated_at = excluded.updated_at;", args.server)
        print(f"demo-{key}: {path.name}")


if __name__ == "__main__":
    main()
