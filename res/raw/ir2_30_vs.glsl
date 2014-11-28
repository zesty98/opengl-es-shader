#version 300 es

#define NUM_OF_INSTNACE 1000

layout(location=0) in vec4 aPosition;
layout(location=2) in vec3 aNormal;
layout(location=3) in vec4 aColor;

out vec4 vColor;
out vec3 vNormal;
out vec4 vPositionES;
out vec4 vLight1PosES;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform highp vec4 uLightPos;

layout (std140) uniform uTranslateBlock {
    vec3 uTranslate[NUM_OF_INSTNACE];
};

layout (std140) uniform uColorBlock {
    vec4 uColor[NUM_OF_INSTNACE];
};

void main() {
    vec4 transPos = (uMMatrix * aPosition) + vec4(uTranslate[gl_InstanceID], 0.0);
    transPos.w = 1.0;

    vec4 posES = uVMatrix * transPos;
    vec4 pos = uPMatrix * posES;

    vPositionES = posES;
    vLight1PosES = uVMatrix * uLightPos;
    vColor = uColor[gl_InstanceID];
    vNormal = aNormal;

    gl_Position = pos;
}
