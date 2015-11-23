#version 300 es

out vec4 vColor;

layout( location = 0) in vec4 aPosition;
layout( location = 2) in vec3 aNormal;
layout( location = 3) in vec4 aColor;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;
uniform highp mat3 uNormalMatrix;

uniform highp vec4 uLightPos;

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

vec4 calcLightColor(vec4 posES, LightInfo lightInfo, MeterialInfo materialInfo) {
    // light position in eye space
    vec4 lightPosES = uVMatrix * uLightPos;

    vec3 lightDirES;

    if (lightPosES.w == 0.0) {
        // directional light
        lightDirES = normalize(lightPosES.xyz);
    } else {
        // point light
        lightDirES = vec3(normalize(lightPosES - posES));
    }

    vec3 normalES = normalize(uNormalMatrix * aNormal);
    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfPlane = normalize(viewDir + lightDirES);

    float diffuse = max(0.0, dot(normalES, lightDirES));
    float specular = max(0.0, dot(normalES, halfPlane));
    specular = pow(specular, materialInfo.specularExponent);

    vec4 lightColor = lightInfo.ambient * materialInfo.ambient
            + lightInfo.diffuse * materialInfo.diffuse * diffuse
            + lightInfo.specular * materialInfo.specular * specular;
    lightColor.w = 1.0;

    return lightColor;
}

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    LightInfo lightInfo = LightInfo(
            vec4(1.0, 1.0, 1.0, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0));

    MeterialInfo materialInfo = MeterialInfo(
            vec4(0.3, 0.3, 0.3, 1.0),
            vec4(0.5, 0.5, 0.5, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0),
            16.0);

    vec4 lightColor = calcLightColor(posES, lightInfo, materialInfo);

    vColor = aColor * lightColor;

    gl_Position = pos;
}