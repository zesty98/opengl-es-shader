#define MAX_LIGHT_NUM 2

precision mediump float;

varying vec2 vTexCoord;
varying vec3 vNormal;
varying vec4 vPositionES;
varying vec4 vLightPosES[MAX_LIGHT_NUM];

uniform sampler2D uTexture;

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

const LightInfo lightInfo = LightInfo(
        vec4(1.0, 1.0, 1.0, 1.0),
        vec4(1.0, 1.0, 1.0, 1.0),
        vec4(1.0, 1.0, 1.0, 1.0));

const MeterialInfo materialInfo = MeterialInfo(
        vec4(0.3, 0.3, 0.3, 1.0),
        vec4(0.5, 0.5, 0.5, 1.0),
        vec4(1.0, 1.0, 1.0, 1.0),
        16.0);

vec4 calcLightColor() {

    vec4 lightColor = sceneAmbient * materialInfo.ambient;

    for (int i = 0; i < MAX_LIGHT_NUM; i++) {
        if (uLightState[i] == 1) {
            vec3 lightDirES;
            if (vLightPosES[i].w == 0.0) {
                // directional light
                lightDirES = normalize(vLightPosES[i].xyz);
            } else {
                // point light
                lightDirES = vec3(normalize(vLightPosES[i] - vPositionES));
            }

            vec3 viewDir = vec3(0.0, 0.0, 1.0);
            vec3 halfPlane = normalize(viewDir + lightDirES);

            vec3 normalES = normalize(uNormalMatrix * vNormal);

            float diffuse = max(0.0, dot(normalES, lightDirES));
            float specular = max(0.0, dot(normalES, halfPlane));
            specular = pow(specular, materialInfo.specularExponent);

            lightColor += lightInfo.diffuse * materialInfo.diffuse * diffuse
                    + lightInfo.specular * materialInfo.specular * specular;
            lightColor.w = 1.0;
        }
    }

    return lightColor;
}

void main() {
    vec4 lightColor = calcLightColor();

    gl_FragColor = texture2D(uTexture, vTexCoord) * lightColor;
}
