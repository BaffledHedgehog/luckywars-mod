"""Prepare a small delta from the current author pack, without changing that pack."""
from pathlib import Path
import argparse
import hashlib
import json
import re

from check_timestop_shaderc import limit_resources


def main():
    limit_resources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', type=Path, required=True)
    args = parser.parse_args()
    out = Path('resourcepack_template/updates/feedback_2026_11_16')
    relative = 'assets/lbc/shaders/include/phone_hd_pixels.glsl'
    raw = (args.pack / relative).read_bytes()
    source = raw.decode('utf-8')
    start = source.index('bool lbc_tiktok_tile(')
    end = source.index('#ifdef LBC_TIKTOK_FRAGMENT', start)
    patched = source[:start] + '''// Model UVs in26.3 include the exact right/bottom atlas boundary.
// Check the four adjacent texels before validating the whole private sprite.
// A standalone texture clamps that boundary and hides this atlas-only failure.
bool lbc_tiktok_tile(sampler2D atlas, vec2 uv, out vec2 local) {
    ivec2 size = textureSize(atlas, 0);
    vec2 pixel = uv * vec2(size);
    ivec2 edge = ivec2(floor(pixel));
    local = vec2(0);
    for (int candidate=0;candidate<4;candidate++) {
        ivec2 p=edge-ivec2(candidate&1,candidate>>1);
        if(any(lessThan(p,ivec2(0)))||any(greaterThanEqual(p,size)))continue;
        ivec4 tag=ivec4(round(texelFetch(atlas,p,0)*255.0));
        if(tag.r!=43 || (tag.a!=211 && tag.a!=255) || tag.g<32 || tag.g>95 || tag.b<128 || tag.b>191)continue;
        ivec2 origin=p-ivec2(tag.g-32,tag.b-128);
        if(any(lessThan(origin,ivec2(0)))||any(greaterThanEqual(origin+63,size)))continue;
        vec2 point=(pixel-vec2(origin))/64.0;
        if(any(lessThan(point,vec2(-.00001)))||any(greaterThan(point,vec2(1.00001))))continue;
        if(any(notEqual(ivec4(round(texelFetch(atlas,origin,0)*255.0)),ivec4(43,32,128,tag.a))) ||
           any(notEqual(ivec4(round(texelFetch(atlas,origin+ivec2(63,0),0)*255.0)),ivec4(43,95,128,tag.a))) ||
           any(notEqual(ivec4(round(texelFetch(atlas,origin+ivec2(0,63),0)*255.0)),ivec4(43,32,191,tag.a))) ||
           any(notEqual(ivec4(round(texelFetch(atlas,origin+ivec2(63,63),0)*255.0)),ivec4(43,95,191,tag.a))))continue;
        local=clamp(point,vec2(0),vec2(1));
        return true;
    }
    return false;
}
''' + source[end:]
    destination = out / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(patched, encoding='utf-8', newline='\n')
    manifest = {'files': [{'path': relative, 'author_base_sha256': hashlib.sha256(raw).hexdigest(),
                           'sha256': hashlib.sha256(destination.read_bytes()).hexdigest()}],
                'authorPackWritten': False}
    relative = 'assets/minecraft/shaders/core/position_tex_color.fsh'
    raw = (args.pack / relative).read_bytes()
    source = raw.decode('utf-8')
    start = source.index('    if(centeredCrosshairScale.x>0.0) {')
    end = source.index('    vec4 color = texture(', start)
    patched = source[:start] + '''    if(centeredCrosshairScale.x>0.0) {
        // Preserve the native GUI sprite and projection. GUI rendering may use
        // an intermediate viewport; Globals.ScreenSize is not its clip rectangle.
        vec4 nativeCrosshair=texture(Sampler0,texCoord0)*vertexColor*ColorModulator;
        if(nativeCrosshair.a<=0.0)discard;
        fragColor=vec4(nativeCrosshair.rgb,251.0/255.0);
        return;
    }
''' + source[end:]
    destination = out / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(patched, encoding='utf-8', newline='\n')
    manifest['files'].append({'path': relative, 'author_base_sha256': hashlib.sha256(raw).hexdigest(),
                              'sha256': hashlib.sha256(destination.read_bytes()).hexdigest()})
    (out / 'export-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(manifest))


if __name__ == '__main__':
    main()
