"""Check native texture decoding/mip preparation in a small, windowless JVM."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from urllib.parse import unquote,urlsplit

ROOT=Path(__file__).resolve().parents[1]


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--overlay',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
    (out/'RESULTS.json').unlink(missing_ok=True)
    cp_file=out/'classpath.txt';init=out/'classpath.init.gradle'
    init.write_text("""allprojects { afterEvaluate { p ->
        if (p == p.rootProject) {
            tasks.register('writeParticleLoaderClasspath') {
                doLast {
                    def cp = p.sourceSets.main.runtimeClasspath
                    def natives = p.configurations.findByName('minecraftNatives')
                    if (natives != null && natives.canBeResolved) cp = cp + natives
                    new File(p.findProperty('particleLoaderClasspathOut')).text = cp.asPath
                }
            }
        }
    } }
""")
    jdk=ROOT.parent/'runtime/jdk-25/bin';env=dict(os.environ,JAVA_HOME=str(jdk.parent))
    inputs={n:hashlib.sha256((ROOT/n).read_bytes()).hexdigest() for n in ('build.gradle','settings.gradle','gradle.properties')}
    cache=out/'classpath-inputs.json'
    if not cp_file.is_file() or not cache.is_file() or json.loads(cache.read_text())!=inputs:
        with (out/'gradle.log').open('wb') as log:
            subprocess.run(['cmd.exe','/d','/c','gradlew.bat','--no-daemon','-I',str(init),
                            '-PparticleLoaderClasspathOut='+str(cp_file),'writeParticleLoaderClasspath'],
                           cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
        cache.write_text(json.dumps(inputs,indent=2)+'\n')
    classes=out/'classes';classes.mkdir(exist_ok=True);classpath=cp_file.read_text()
    with (out/'native.log').open('wb') as log:
        subprocess.run([str(jdk/'javac.exe'),'-J-Xmx128m','-proc:none','-cp',classpath,'-d',str(classes),
                        str(ROOT/'tools/ParticleSpriteContract.java')],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        natives=out/'natives';natives.mkdir(exist_ok=True)
        subprocess.run([str(jdk/'java.exe'),'-Xms32m','-Xmx128m','-Djava.awt.headless=true',
                        '-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(natives),
                        '-cp',str(classes)+';'+classpath,'ParticleSpriteContract',
                        str((args.overlay/'assets/minecraft/textures/particle/flash.png').resolve()),
                        str((args.overlay/'assets/minecraft/textures/particle/flash.png.mcmeta').resolve()),
                        str(out/'RESULTS.json')],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
    result=json.loads((out/'RESULTS.json').read_text());assert result['passed']
    result['inputs']={name:hashlib.sha256((args.overlay/name).read_bytes()).hexdigest()
        for name in ('assets/minecraft/textures/particle/flash.png','assets/minecraft/textures/particle/flash.png.mcmeta')}
    origin=urlsplit(result['nativeCodeSource']);assert origin.scheme=='file' and not origin.netloc
    native_jar=Path(unquote(origin.path).lstrip('/'))
    with native_jar.open('rb') as stream:result['nativeCodeSourceSha256']=hashlib.file_digest(stream,'sha256').hexdigest()
    result['javaSourceSha256']=hashlib.sha256((ROOT/'tools/ParticleSpriteContract.java').read_bytes()).hexdigest()
    (out/'RESULTS.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))


if __name__=='__main__':
    try:main()
    except subprocess.CalledProcessError as error:
        raise SystemExit('Native sprite check child exit '+str(error.returncode)+'; inspect gradle.log/native.log in the output directory') from None
