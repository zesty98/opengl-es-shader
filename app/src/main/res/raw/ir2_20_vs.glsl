attribute vec4 aPosition;
attribute vec3 aNormal;

varying vec4 vColor;
varying vec3 vNormal;
varying vec4 vPositionES;
varying vec4 vLightPosES[8];

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp int uLightState[8];
uniform highp vec4 uLightPos[8];

uniform highp vec4 uColor;

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    vPositionES = posES;
    for (int i = 0; i < 8; i++) {
        if (uLightState[i] == 1) {
            vLightPosES[i] = uVMatrix * uLightPos[i];
        }
    }
    vColor = uColor;
    vNormal = aNormal;

    gl_Position = pos;
}
