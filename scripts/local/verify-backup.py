import hashlib
import json
from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()
manifest = json.loads((root / 'manifest.json').read_text())
if 'database.dump' not in manifest or not (root / 'storage').is_dir():
    raise ValueError('La copia no incluye base e imágenes.')
for name, expected in manifest.items():
    path = (root / name).resolve()
    if root not in path.parents:
        raise ValueError('Ruta fuera de la copia.')
    with path.open('rb') as source:
        if hashlib.file_digest(source, 'sha256').hexdigest() != expected:
            raise ValueError(f'Archivo alterado: {name}')
print('Integridad de la copia verificada.')
