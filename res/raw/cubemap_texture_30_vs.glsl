#version 300 es

layout(location=0) in vec4 aPosition;
layout(location=2) in vec3 aNormal;

out vec3 vLookupVec;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vLookupVec = aPosition.xyz;

    gl_Position = pos;
}
