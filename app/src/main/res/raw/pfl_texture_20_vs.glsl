#define MAX_LIGHT_NUM 2

attribute vec4 aPosition;
attribute vec2 aTexCoord;
attribute vec3 aNormal;

varying vec2 vTexCoord;
varying vec3 vNormal;
varying vec4 vPositionES;
varying vec4 vLightPosES[MAX_LIGHT_NUM];

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp int uLightState[MAX_LIGHT_NUM];
uniform highp vec4 uLightPos[MAX_LIGHT_NUM];

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    vPositionES = posES;
    vPositionES = posES;
    for (int i = 0; i < MAX_LIGHT_NUM; i++) {
        if (uLightState[i] == 1) {
            vLightPosES[i] = uVMatrix * uLightPos[i];
        }
    }
    vTexCoord = aTexCoord;
    vNormal = aNormal;

    gl_Position = pos;
}
