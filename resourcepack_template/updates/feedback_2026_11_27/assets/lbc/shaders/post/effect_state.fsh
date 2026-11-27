#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
#include <lbc:effect_packet.glsl>

uniform sampler2D CurrentSampler;
uniform sampler2D HistorySampler;
layout(location = 0) out vec4 fragColor;

// Tiny persistent object records, never previous world RGB. Durations are
// seconds and are independent of FPS, particle age and display scale.
const float BH_FADE_SECONDS=0.20;
// A lost outline packet must not close a still-live hole.
const float BH_MISSING_GRACE_SECONDS=0.10;
const float NUCLEAR_FADE_OUT=12.0;
const float BH_SPAWN_BACK=1.70158; // About 10% overshoot, then settle.
const ivec2 EFFECT_SIZE=ivec2(171,LBC_EFFECT_ROWS);

uint readBits(ivec2 pixel) {
    return lbc_packet_unpack(texelFetch(HistorySampler,pixel,0));
}
float oldFloat(ivec2 pixel) { return uintBitsToFloat(readBits(pixel)); }
int nuclearView() {
    return lbc_packet_record(CurrentSampler, LBC_PACKET_NUCLEAR_VIEW, LBC_PACKET_MATRIX, LBC_PACKET_NUCLEAR, LBC_PACKET_FADE);
}
int cameraRow() {
    int hole = lbc_packet_record(CurrentSampler, LBC_PACKET_HOLE_VIEW, LBC_PACKET_MATRIX, LBC_PACKET_HOLE, LBC_PACKET_DISK);
    if (hole >= 0) return hole;
    int nuclear = nuclearView();
    if (nuclear >= 0) return nuclear;
    return lbc_packet_record(CurrentSampler, LBC_PACKET_HOLE_VIEW, LBC_PACKET_MATRIX, LBC_PACKET_ZONE, LBC_PACKET_ZONE);
}
ivec3 rawBytes(int x,int row) {
    int kind = lbc_packet_kind(CurrentSampler, row);
    if (x == 56) return lbc_packet_header_rgb(kind);
    int field = x / 2;
    if (field >= LBC_PACKET_POINTS) {
        if (row % LBC_PACKET_SIZE < LBC_PACKET_HOLE_VIEW) {
            row = kind == LBC_PACKET_NUCLEAR || kind == LBC_PACKET_FADE ? nuclearView() : -1;
            if (row < 0) row = cameraRow();
        }
        field -= LBC_PACKET_POINTS;
    }
    uint word = row >= 0 ? lbc_packet_read(CurrentSampler, row + 1 + field) : 0u;
    uint shift = x % 2 == 0 ? 0u : 16u;
    return ivec3(int((word >> shift) & 255u), int((word >> (shift + 8u)) & 255u), 201 + x % 2);
}
bool header(ivec3 b,int row) {
    return row<LBC_NUCLEAR_FIRST_ROW ? b.r==66&&b.g==72&&(b.b==100||b.b==101) :
        lbc_nuclear_header(b,row>=LBC_NUCLEAR_FADE_FIRST_ROW);
}
int currentRow(int row) {
    if (row < LBC_NUCLEAR_FIRST_ROW) {
        int offset = (row - LBC_BH_FIRST_ROW) * LBC_PACKET_OBJECT;
        return lbc_packet_record(CurrentSampler, offset, LBC_PACKET_POINTS, LBC_PACKET_HOLE, LBC_PACKET_DISK);
    }
    bool fading = row >= LBC_NUCLEAR_FADE_FIRST_ROW;
    int slot = row - (fading ? LBC_NUCLEAR_FADE_FIRST_ROW : LBC_NUCLEAR_FIRST_ROW);
    int offset = (LBC_BH_COUNT + slot) * LBC_PACKET_OBJECT;
    int kind = fading ? LBC_PACKET_FADE : LBC_PACKET_NUCLEAR;
    return lbc_packet_record(CurrentSampler, offset, LBC_PACKET_POINTS, kind, kind);
}
float pointField(int field,int row,bool current) {
    if (current) return uintBitsToFloat(lbc_packet_read(CurrentSampler, row + 1 + field));
    ivec3 a=ivec3(round(texelFetch(HistorySampler,ivec2(field*2,row),0).rgb*255.0));
    ivec3 b=ivec3(round(texelFetch(HistorySampler,ivec2(field*2+1,row),0).rgb*255.0));
    return uintBitsToFloat(uint(a.r)|(uint(a.g)<<8u)|(uint(b.r)<<16u)|(uint(b.g)<<24u));
}
vec3 point(int index,int row,bool current) {
    return vec3(pointField(index*3,row,current),pointField(index*3+1,row,current),pointField(index*3+2,row,current));
}
vec4 encodedField(float value,int x) {
    uint bits=floatBitsToUint(value);
    uint shift=(x%2==0)?0u:16u;
    return vec4(float((bits>>shift)&255u),float((bits>>(shift+8u))&255u),x%2==0?201.0:202.0,255.0)/255.0;
}
float holeArrival(float phase) {
    float t=phase-1.0;
    return 1.0+(BH_SPAWN_BACK+1.0)*t*t*t+BH_SPAWN_BACK*t*t;
}
float nuclearOpacity(float progress,float visibility) {
    // Quantize identically for UNORM header alpha and the float cache. A
    // half-byte tie must not yield different opacity in the two draw paths.
    float opacity=smoothstep(0.0,1.0,progress)*visibility;
    opacity=floor(clamp(opacity,0.0,1.0)*65536.0+0.5)/65536.0;
    return floor(opacity*255.0+0.5)/255.0;
}
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    int x=pixel.x,row=min(pixel.y,EFFECT_SIZE.y-1-pixel.y);
    fragColor=vec4(0.0);
    bool history=all(equal(ivec4(round(texelFetch(HistorySampler,ivec2(7,1),0)*255.0)),ivec4(76,73,70,69)));
    float now=GameTime*1200.0;
    // ClientLevel game time advances locally but the server may correct it
    // backwards under low TPS. A correction is not elapsed time and must not
    // invalidate live effect history. Preserve the ordinary 1200s wrap.
    float elapsed=history?now-oldFloat(ivec2(6,1)):0.0;
    if(elapsed < -600.0)elapsed+=1200.0;
    float dt=elapsed>=0.0&&elapsed<=2.0?elapsed:0.0;
    ivec3 oldBlock=ivec3(int(readBits(ivec2(0,1))),int(readBits(ivec2(1,1))),int(readBits(ivec2(2,1))));
    vec3 offset=vec3(oldFloat(ivec2(3,1)),oldFloat(ivec2(4,1)),oldFloat(ivec2(5,1)));
    vec3 delta=vec3(oldBlock-CameraBlockPos)+offset-CameraOffset;
    history=history&&length(delta)<256.0;
    if(!history)dt=0.0;

    if(row==1) {
        if(x<3)fragColor=lbc_packet_pack(uint(CameraBlockPos[x]));
        else if(x<6)fragColor=lbc_packet_pack(floatBitsToUint(CameraOffset[x-3]));
        else if(x==6)fragColor=lbc_packet_pack(floatBitsToUint(now));
        else if(x==7)fragColor=vec4(76,73,70,69)/255.0;
        return;
    }
    if(row==0) {
        if(x>=24&&x<56) {
            int live=cameraRow();
            fragColor=live>=0?vec4(vec3(rawBytes(x,live))/255.0,1.0):
                                 (history?texelFetch(HistorySampler,ivec2(x,0),0):vec4(0.0));
        }
        return;
    }
    bool nuclear=row>=LBC_NUCLEAR_FIRST_ROW;
    int live=currentRow(row);
    bool fading=false;
    if(nuclear&&live<0) {
        live=currentRow(row+LBC_NUCLEAR_COUNT);
        fading=live>=0;
    }
    float nuclearVisibility=1.0,nuclearDuration=NUCLEAR_FADE_OUT,nuclearTotal=0.0,serverAge=0.0;
    bool timedMain=false;
    if(nuclear&&live>=0) {
        vec3 origin=point(0,live,true);
        float ratio=length(point(3,live,true)-origin)/max(length(point(1,live,true)-origin),0.0001);
        // Fade-only duration channel: Z/R = 2 + seconds. Legacy Z/R=1..2
        // still controls visibility; changing duration never stretches smoke.
        bool customDuration=fading&&ratio>=2.5;
        timedMain=!fading&&ratio>=2.5;
        nuclearVisibility=(customDuration||timedMain)?1.0:clamp(2.0-ratio,0.0,1.0);
        if(customDuration)nuclearDuration=clamp(ratio-2.0,0.5,120.0);
        if(timedMain)nuclearTotal=clamp(ratio-2.0,0.5,120.0);
        serverAge=clamp(length(point(2,live,true)-origin)-1.0,0.0,120.0);
    }
    bool wasAlive=history&&header(ivec3(round(texelFetch(HistorySampler,ivec2(56,row),0).rgb*255.0)),row);
    float progress=wasAlive?oldFloat(ivec2(57,row)):0.0;
    if(isnan(progress)||isinf(progress))progress=0.0;
    progress=clamp(progress,0.0,1.0);
    float previousProgress=progress;
    float missing=0.0;
    if(!nuclear && live<0 && wasAlive) {
        float previousMissing=oldFloat(ivec2(60,row));
        if(isnan(previousMissing)||isinf(previousMissing)||previousMissing<0.0)previousMissing=0.0;
        missing=min(previousMissing+dt,BH_MISSING_GRACE_SECONDS+BH_FADE_SECONDS);
    }
    if(live>=0&&wasAlive) {
        vec3 oldCenter=point(0,row,false)+delta;
        float radius=length(point(1,live,true)-point(0,live,true));
        bool replaced=(!nuclear&&any(notEqual(rawBytes(56,live),ivec3(round(texelFetch(HistorySampler,ivec2(56,row),0).rgb*255.0)))))||
                      distance(oldCenter,point(0,live,true))>max(radius*4.0,8.0);
        if(replaced){progress=0.0;wasAlive=false;}
    }
    float wallAge=serverAge;
    if(nuclear && live>=0) {
        float priorTotal=wasAlive?oldFloat(ivec2(78,row)):0.0;
        float priorAge=wasAlive?oldFloat(ivec2(77,row)):0.0;
        if(isnan(priorTotal)||isinf(priorTotal)||priorTotal<0.0||priorTotal>120.0)priorTotal=0.0;
        if(isnan(priorAge)||isinf(priorAge)||priorAge<0.0||priorAge>120.0)priorAge=0.0;
        if(fading&&priorTotal>0.0)nuclearTotal=priorTotal;
        if(nuclearTotal>0.0) {
            wallAge=wasAlive&&priorTotal>0.0?max(priorAge+dt,serverAge):serverAge;
            nuclearDuration=fading?nuclearDuration:(nuclearTotal<=10.5?4.0:12.0);
            if(fading&&wasAlive&&oldFloat(ivec2(59,row))<1.5)
                wallAge=max(wallAge,nuclearTotal-nuclearDuration);
            wallAge=min(wallAge,120.0);
        }
    }
    if(nuclear) {
        // A real, stationary fade display supplies ALL points and inverse VP
        // every frame. Never render a nuclear tail from a dead carrier's VP.
        if(live<0)progress=0.0;
        else if(nuclearTotal>0.0)progress=clamp((nuclearTotal-wallAge)/nuclearDuration,0.0,1.0);
        else if(!fading)progress=1.0;
        else if(!wasAlive||oldFloat(ivec2(59,row))<1.5)progress=1.0;
        else progress=max(progress-dt/nuclearDuration,0.0);
    }
    else if(live>=0)progress=min(progress+(wasAlive?dt/BH_FADE_SECONDS:0.0),1.0);
    else {
        // A removal during arrival still gets its complete exit duration,
        // beginning at the actual displayed radius (including overshoot).
        if(wasAlive&&oldFloat(ivec2(59,row))>0.5){progress=1.0;previousProgress=1.0;}
        float fadeDt=max(missing-BH_MISSING_GRACE_SECONDS,0.0)-max(missing-dt-BH_MISSING_GRACE_SECONDS,0.0);
        progress=max(progress-fadeDt/BH_FADE_SECONDS,0.0);
    }
    bool nuclearDissolving=fading||(nuclear&&nuclearTotal>0.0&&wallAge>nuclearTotal-nuclearDuration);
    float dissolve=nuclearDissolving?1.0-progress:0.0;
    // Keep the cap fully opaque while the clearing front climbs the stem.
    float opacityProgress=nuclearDissolving?min(progress/(1.0-LBC_NUCLEAR_DISSOLVE_SPLIT),1.0):progress;
    if(nuclear&&x==77){fragColor=lbc_packet_pack(floatBitsToUint(wallAge));return;}
    if(nuclear&&x==78){fragColor=lbc_packet_pack(floatBitsToUint(nuclearTotal));return;}
    if(!nuclear && x==60) {fragColor=lbc_packet_pack(floatBitsToUint(missing));return;}
    if(x==57) {fragColor=lbc_packet_pack(floatBitsToUint(progress));return;}
    if(x==59) {fragColor=lbc_packet_pack(floatBitsToUint(live>=0?(fading?2.0:1.0):0.0));return;}
    if(nuclear&&x>=60&&x<=76&&live>=0) {
        // Prepare exact float object data once per frame for the 5..16 path.
        // The fragment stage fetches only its current object; no huge arrays
        // of matrices need to cross the vertex/fragment interface.
        int source=live>=0?live:row;
        bool current=live>=0;
        vec3 origin=point(0,source,current);
        vec3 axisX=point(1,source,current)-origin;
        vec3 axisY=point(2,source,current)-origin;
        vec3 axisZ=point(3,source,current)-origin;
        float radius=length(axisX),value=0.0;
        if(x<63)value=(origin+(current?vec3(0.0):delta))[x-60];
        else if(x==63)value=radius;
        else if(x<73) {
            mat3 rotation=transpose(mat3(normalize(axisX),normalize(axisY),normalize(axisZ)));
            value=rotation[(x-64)/3][(x-64)%3];
        } else if(x==73)value=nuclearTotal>0.0?wallAge:clamp(length(axisY)-1.0,0.0,120.0);
        else if(x==74) {
            value=nuclearOpacity(opacityProgress,nuclearVisibility);
        } else if(x==75)value=float(row-LBC_NUCLEAR_FIRST_ROW)*13.71;
        else value=dissolve;
        fragColor=lbc_packet_pack(floatBitsToUint(value));return;
    }
    if(x>58||(live<0&&(!wasAlive||progress<=0.0)))return;
    if(x==56||x==58) {
        // Optional server-controlled visibility while the carrier is alive:
        // Z/X=1 means fully visible (existing commands), Z/X=2 means hidden.
        // Keeping the live carrier until zero also supplies fresh camera data.
        int source=live>=0?live:row;
        vec3 origin=point(0,source,live>=0);
        float ratio=length(point(3,source,live>=0)-origin)/max(length(point(1,source,live>=0)-origin),0.0001);
        float visibility=nuclear?nuclearVisibility:clamp(2.0-ratio,0.0,1.0);
        float fade=smoothstep(0.0,1.0,progress);
        if(x==58) {
            // Store radius in a float: RGBA8 alpha cannot carry overshoot >1.
            // If removed during the bounce, contract from the actual last size.
            float size=live>=0?holeArrival(progress)*visibility:
                oldFloat(ivec2(58,row))*fade/max(smoothstep(0.0,1.0,previousProgress),1e-6);
            fragColor=lbc_packet_pack(floatBitsToUint(nuclear?nuclearOpacity(opacityProgress,visibility):max(size,0.0)));
        } else {
            // Normalize both nuclear inputs to the existing NUK output ABI.
            // The volume, resolve and heat shaders keep exactly their design.
            vec3 bytes=nuclear?vec3(78,85,75)/255.0:
                (live>=0?vec3(rawBytes(x,live))/255.0:texelFetch(HistorySampler,ivec2(x,row),0).rgb);
            fragColor=vec4(bytes,nuclear?nuclearOpacity(opacityProgress,visibility):fade*visibility);
        }
    } else if(live>=0&&nuclear&&nuclearTotal>0.0&&x>=12&&x<18) {
        vec3 center=point(0,live,true);
        vec3 axis=point(2,live,true)-center;
        vec3 p=center+normalize(axis)*(wallAge+1.0);
        fragColor=encodedField(p[(x/2)%3],x);
    } else if(live>=0)fragColor=vec4(vec3(rawBytes(x,live))/255.0,1.0);
    else if(x<24) {
        int index=x/6,axis=(x/2)%3;
        vec3 p=point(index,row,false)+delta;
        // Only black holes use a brief tail without a live carrier.
        fragColor=encodedField(p[axis],x);
    } else {
        int camera=cameraRow();
        fragColor=camera>=0?vec4(vec3(rawBytes(x,camera))/255.0,1.0):texelFetch(HistorySampler,ivec2(x,0),0);
    }
}
