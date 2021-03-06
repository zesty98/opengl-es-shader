#version 300 es

#define MAX_LIGHT_NUM 2

precision mediump float;

in vec4 vColor;
in vec3 vNormal;
in vec4 vPositionES;
in vec4 vLight1PosES;
in vec4 vLight2PosES;

layout( location = 0) out vec4 fragColor;

uniform highp mat3 uNormalMatrix;

uniform lowp int uLightState[MAX_LIGHT_NUM];

struct LightInfo {
    highp vec4 ambient;
    highp vec4 diffuse;
    highp vec4 specular;
};

struct MeterialInfo {
    highp vec4 ambient;
    highp vec4 diffuse;
    highp vec4 specular;
    highp float specularExponent;
};

const vec4 sceneAmbient = vec4(1.0, 1.0, 1.0, 1.0);

//vec4 calcLightColor() {
//
//    vec4 lightColor = sceneAmbient * materialInfo.ambient;
//
//    for (int i = 0; i < 8; i++) {
//        if (uLightState[i] == 1) {
//            vec3 lightDirES;
//            if (vLightPosES[i].w == 0.0) {
//                // directional light
//                lightDirES = normalize(vLightPosES[i].xyz);
//            } else {
//                // point light
//                lightDirES = vec3(normalize(vLightPosES[i] - vPositionES));
//            }
//
//            vec3 viewDir = vec3(0.0, 0.0, 1.0);
//            vec3 halfPlane = normalize(viewDir + lightDirES);
//
//            vec3 normalES = normalize(uNormalMatrix * vNormal);
//
//            float diffuse = max(0.0, dot(normalES, lightDirES));
//            float specular = max(0.0, dot(normalES, halfPlane));
//            specular = pow(specular, materialInfo.specularExponent);
//
//            lightColor += lightInfo.diffuse * materialInfo.diffuse * diffuse
//                    + lightInfo.specular * materialInfo.specular * specular;
//            lightColor.w = 1.0;
//        }
//    }
//
//    return lightColor;
//}

vec4 calcLightColor(vec4 lightPosES, LightInfo lightInfo, MeterialInfo materialInfo) {
    vec3 lightDirES;
    if (lightPosES.w == 0.0) {
        // directional light
        lightDirES = normalize(lightPosES.xyz);
    } else {
        // point light
        lightDirES = vec3(normalize(lightPosES - vPositionES));
    }

    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfPlane = normalize(viewDir + lightDirES);

    vec3 normalES = normalize(uNormalMatrix * vNormal);

    float diffuse = max(0.0, dot(normalES, lightDirES));
    float specular = max(0.0, dot(normalES, halfPlane));
    specular = pow(specular, materialInfo.specularExponent);

    vec4 lightColor = lightInfo.diffuse * materialInfo.diffuse * diffuse
            + lightInfo.specular * materialInfo.specular * specular;
    lightColor.w = 1.0;

    return lightColor;
}

void main() {
    LightInfo lightInfo = LightInfo(
            vec4(1.0, 1.0, 1.0, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0));

    MeterialInfo materialInfo = MeterialInfo(
            vec4(0.3, 0.3, 0.3, 1.0),
            vec4(0.5, 0.5, 0.5, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0),
            16.0);

    vec4 lightColor = sceneAmbient * materialInfo.ambient;

    if (uLightState[0] == 1) {
        lightColor += calcLightColor(vLight1PosES, lightInfo, materialInfo);
    }

    if (uLightState[1] == 1) {
        lightColor += calcLightColor(vLight2PosES,lightInfo, materialInfo);
    }

    fragColor = vColor * lightColor;
}
