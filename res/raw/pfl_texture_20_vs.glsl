attribute vec4 aPosition;
attribute vec2 aTexCoord;
attribute vec3 aNormal;

varying vec2 vTexCoord;
varying vec3 vNormal;
varying vec4 vPositionES;
varying vec4 vLightPosES[8];

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp int uLightState[8];
uniform highp vec4 uLightPos[8];

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    vPositionES = posES;
    vPositionES = posES;
    for (int i = 0; i < 8; i++) {
        if (uLightState[i] == 1) {
            vLightPosES[i] = uVMatrix * uLightPos[i];
        }
    }
    vTexCoord = aTexCoord;
    vNormal = aNormal;

    gl_Position = pos;
}
