#ifndef LBC_HAKUREI_SPHERE_GLSL
#define LBC_HAKUREI_SPHERE_GLSL

void lbc_hakurei_hit(float distance, vec4 sphere, float occlusion,
                     inout vec2 distances, inout vec4 nearSphere, inout vec4 farSphere) {
    if (distance < 0.0 || distance > occlusion) return;
    if (distance < distances.x) {
        distances.y = distances.x;
        farSphere = nearSphere;
        distances.x = distance;
        nearSphere = sphere;
    } else if (distance < distances.y) {
        distances.y = distance;
        farSphere = sphere;
    }
}

vec3 lbc_hakurei_surface(vec3 background, float distance, vec4 sphere,
                         ZoneRay ray, ZoneRay rayX, ZoneRay rayY) {
    vec3 position = ray.origin + ray.direction * distance;
    vec3 normal = normalize(position - sphere.xyz);
    vec4 energy = lbc_zone_color(position, zoneHitDifferential(distance, normal, ray, rayX),
                                 zoneHitDifferential(distance, normal, ray, rayY), sphere.w > 0.0 ? 1 : 2);
    return mix(background, energy.rgb, energy.a * 0.72);
}

vec3 lbc_hakurei_render(vec3 color, ZoneRay ray, ZoneRay rayX, ZoneRay rayY, float occlusion) {
    vec2 distances = vec2(1e30);
    vec4 nearSphere = vec4(0.0);
    vec4 farSphere = vec4(0.0);
    for (int i = 0; i < int(zoneInfo.z); i++) {
        vec4 sphere = hakureiSpheres[i];
        vec3 offset = ray.origin - sphere.xyz;
        float middle = -dot(offset, ray.direction);
        vec3 closest = offset + ray.direction * middle;
        float discriminant = sphere.w * sphere.w - dot(closest, closest);
        if (discriminant < 0.0) continue;
        float span = sqrt(discriminant);
        lbc_hakurei_hit(middle - span, sphere, occlusion, distances, nearSphere, farSphere);
        if (span > 0.00001) lbc_hakurei_hit(middle + span, sphere, occlusion, distances, nearSphere, farSphere);
    }
    // Resolve the two frontmost visible translucent surfaces. The ray tests
    // remain cheap even when several cloned spheres overlap on one pixel.
    if (distances.y < 1e30) color = lbc_hakurei_surface(color, distances.y, farSphere, ray, rayX, rayY);
    if (distances.x < 1e30) color = lbc_hakurei_surface(color, distances.x, nearSphere, ray, rayX, rayY);
    return color;
}

#endif
