#version 300 es

#define MAX_LIGHT_NUM 2

layout( location = 0) in vec4 aPosition;
layout( location = 1) in vec2 aTexCoord;
layout( location = 2) in vec3 aNormal;

out vec2 vTexCoord;
out vec3 vNormal;
out vec4 vPositionES;
out vec4 vLight1PosES;
out vec4 vLight2PosES;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform highp vec4 uLightPos;
uniform highp vec4 uLight2Pos;
uniform lowp int uLightState[MAX_LIGHT_NUM];

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    vPositionES = posES;

    if (uLightState[0] == 1) {
        vLight1PosES = uVMatrix * uLightPos;
    }

    if (uLightState[1] == 1) {
        vLight2PosES = uVMatrix * uLight2Pos;
    }

    vTexCoord = aTexCoord;
    vNormal = aNormal;

    gl_Position = pos;
}
