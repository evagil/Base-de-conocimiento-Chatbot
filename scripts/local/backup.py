"""Back up the database snapshot first, then its immutable image files."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
from urllib.parse import urlsplit


def backup():
    os.umask(0o077)
    project = Path(__file__).resolve().parents[2]
    destination = Path(os.environ.get('BACKUP_PATH', str(project / 'backups'))).resolve()
    storage = Path(os.environ['STORAGE_PATH']).resolve()
    if not storage.is_dir():
        raise ValueError('No existe STORAGE_PATH; se cancela para no crear una copia incompleta.')
    if destination == storage or storage in destination.parents:
        raise ValueError('BACKUP_PATH debe estar fuera de STORAGE_PATH.')
    url = urlsplit(os.environ['DB_URL'].removeprefix('jdbc:'))
    if url.scheme != 'postgresql' or not url.hostname or url.query or url.username or not url.path.strip('/'):
        raise ValueError('DB_URL debe tener formato jdbc:postgresql://host:puerto/base sin parámetros.')
    destination.mkdir(parents=True, exist_ok=True)
    staging = Path(tempfile.mkdtemp(prefix='.incompleto-', dir=destination))
    try:
        env = dict(os.environ, PGPASSWORD=os.environ['DB_PASSWORD'])
        subprocess.run(['pg_dump', '--no-password', '--format=custom', '--no-owner', '--no-privileges',
                        '--host', url.hostname, '--port', str(url.port or 5432),
                        '--username', os.environ['DB_USERNAME'], '--dbname', url.path[1:],
                        '--file', str(staging / 'database.dump')], env=env, check=True)
        # Uploaded files are immutable and created before their database transaction commits.
        # Logical deletion retains the physical file, so later copying covers the DB snapshot.
        if any(path.is_symlink() for path in storage.rglob('*')):
            raise ValueError('El almacenamiento contiene enlaces simbólicos; revisarlo antes de copiar.')
        shutil.copytree(storage, staging / 'storage')
        manifest = {}
        for path in sorted(staging.rglob('*')):
            if path.is_file():
                with path.open('rb') as source:
                    manifest[str(path.relative_to(staging))] = hashlib.file_digest(source, 'sha256').hexdigest()
        (staging / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
        name = datetime.datetime.now(datetime.timezone.utc).strftime('copia-%Y%m%dT%H%M%S.%fZ')
        completed = destination / name
        staging.rename(completed)
        print(f'Copia completa: {completed}')
    except BaseException:
        shutil.rmtree(staging)
        raise


if __name__ == '__main__':
    backup()
