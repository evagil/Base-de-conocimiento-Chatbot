"""Install per-user units. Invoked explicitly by the local setup script."""
import json
import os
from pathlib import Path
import shutil

project = Path(__file__).resolve().parents[2]
if '\n' in str(project) or '\r' in str(project):
    raise ValueError('La ruta del proyecto no puede contener saltos de línea.')
units = Path(os.environ.get('XDG_CONFIG_HOME', str(Path.home() / '.config'))) / 'systemd/user'
units.mkdir(parents=True, exist_ok=True)
# Escape systemd specifiers as well as quoted string syntax (paths can contain spaces).
def quote(value):
    return json.dumps(str(value).replace('%', '%%'))

for action in ('backend', 'frontend', 'backup'):
    unit = f'''[Unit]
Description=Base de conocimiento - {action}

[Service]
Type={'oneshot' if action == 'backup' else 'simple'}
WorkingDirectory={str(project).replace('%', '%%')}
Environment={quote('PATH=' + os.environ['PATH'])}
ExecStart={quote(shutil.which('bash'))} {quote(project / 'scripts/local/run.sh')} {action}
UMask=0077
'''
    if action != 'backup':
        unit += 'Restart=on-failure\nRestartSec=10\n\n[Install]\nWantedBy=default.target\n'
    (units / f'base-conocimiento-{action}.service').write_text(unit)
(units / 'base-conocimiento-backup.timer').write_text('''[Unit]
Description=Copia diaria de la base de conocimiento

[Timer]
OnCalendar=daily
Persistent=true
RandomizedDelaySec=300

[Install]
WantedBy=timers.target
''')
