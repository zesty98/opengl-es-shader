#version 300 es

layout(location=0) in vec4 aPosition;
layout(location=2) in vec3 aNormal;

out vec3 vLookupVec;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;
uniform highp mat3 uNormalMatrix;
uniform highp vec4 uEyePos;

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    vec3 normalES = normalize(uNormalMatrix * aNormal);

    vec4 eyePosES = uVMatrix * uMMatrix * uEyePos;
    vec4 eyeDir = posES - eyePosES;
    vLookupVec = reflect(eyeDir.xyz, normalES);

    gl_Position = pos;
}
