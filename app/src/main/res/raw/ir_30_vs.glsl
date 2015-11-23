#version 300 es

layout( location = 0) in vec4 aPosition;
layout( location = 2) in vec3 aNormal;
layout( location = 3) in vec4 aColor;
layout( location = 4) in vec3 aTranslate;

out vec4 vColor;
out vec3 vNormal;
out vec4 vPositionES;
out vec4 vLight1PosES;
out vec4 vLight2PosES;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform highp vec4 uLightPos;

void main() {
    vec4 transPos = uMMatrix * aPosition + vec4(aTranslate, 0.0);
    vec4 posES = uVMatrix * transPos;
    vec4 pos = uPMatrix * posES;

    vPositionES = posES;
    vLight1PosES = uVMatrix * uLightPos;
    vColor = aColor;
    vNormal = aNormal;

    gl_Position = pos;
}
