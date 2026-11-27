#ifndef LBC_NUCLEAR_TRACE_GLSL
#define LBC_NUCLEAR_TRACE_GLSL
// Shared by the coarse plume and full-resolution stem refinement. Both read
// the same depth and integrate premultiplied radiance/transmittance only.
vec4 nuclearTrace(vec3 origin, vec3 direction, float sceneDistance, vec2 pixel) {
    vec3 radiance=vec3(0.0);
    float transmittance=1.0;
    vec2 intervals[LBC_NUCLEAR_TRACE_COUNT];
    int order[LBC_NUCLEAR_TRACE_COUNT];
    float time=GameTime*1200.0;
    for(int i=0;i<nuclearCount;i++) {
        NuclearObject object=nuclearGetObject(i);
        float radius=object.sphere.w*NUCLEAR_WORLD_SCALE;
        vec3 dim=nuclearDimensions(object.age);
        vec3 ro=object.rotation*(origin-object.sphere.xyz)/radius;
        vec3 rd=object.rotation*direction/radius;
        intervals[i]=nuclearInterval(ro,rd,dim,object.age);
        intervals[i].x=max(intervals[i].x,0.0);
        intervals[i].y=min(intervals[i].y,sceneDistance);
        order[i]=i;
    }
    for(int i=0;i<nuclearCount;i++) for(int j=i+1;j<nuclearCount;j++) {
        if(intervals[order[j]].x<intervals[order[i]].x) {
            int temporary=order[i];order[i]=order[j];order[j]=temporary;
        }
    }
    for(int k=0;k<nuclearCount;k++) {
        int i=order[k];
        if(intervals[i].y<=intervals[i].x)continue;
        NuclearObject object=nuclearGetObject(i);
        float age=object.age;
        vec3 dim=nuclearDimensions(age);
        float fade=object.fade*(1.0-smoothstep(66.0,70.0,age));
        if(age>=70.0||transmittance<0.002||fade<=0.0)continue;
        float radius=object.sphere.w*NUCLEAR_WORLD_SCALE;
        vec3 ro=object.rotation*(origin-object.sphere.xyz)/radius;
        vec3 rd=object.rotation*direction;
        float start=intervals[i].x/radius,end=intervals[i].y/radius;
        if(end<=start)continue;
        vec3 beforeRadiance=radiance;
        float beforeTransmittance=transmittance;
        float stepSize=max((end-start)/48.0,0.003);
        float t=start+stepSize*nuclearHash(vec3(floor(pixel),0.0));
        for(int step=0;step<48;step++) {
            if(t>end||transmittance<0.002)break;
            vec3 p=ro+rd*t;
            vec4 medium=nuclearMedium(p,age,time,object.seed);
            float dustR=nuclearDustRadius(age);
            float dust=exp(-pow((length(p.xz*vec2(1.0,4.0/3.0))-dustR)/nuclearDustWidth(age),2.0)-pow((p.y+0.08)/0.40,2.0))*
                       smoothstep(0.2,1.2,age)*(1.0-smoothstep(25.0,48.0,age));
            float materialDensity=medium.x+dust*1.9;
            float density=materialDensity*nuclearDissolve(p,dim,object.dissolve,time,object.seed);
            float opacity=1.0-exp(-density*stepSize);
            vec3 scatter=mix(vec3(0.17,0.145,0.12),vec3(0.76,0.69,0.57),medium.w);
            scatter*=0.55+medium.z*0.75;
            scatter=mix(scatter,vec3(0.40,0.31,0.20),dust/max(materialDensity,0.01));
            vec3 fire=nuclearFire(medium.y)*pow(medium.y,2.5)*1.12;
            radiance+=transmittance*opacity*(scatter+fire);
            transmittance*=1.0-opacity;
            // Skip low-density space without increasing the maximum steps.
            t+=stepSize*(density<0.02?1.65:1.0);
        }
        // Fade the completed layer, preserving the shape and its contrast.
        radiance=mix(beforeRadiance,radiance,fade);
        transmittance=mix(beforeTransmittance,transmittance,fade);
    }
    return vec4(radiance,transmittance);
}
#endif
