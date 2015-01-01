#version 300 es

precision mediump float;

out vec2 vTexCoord;
out vec2 vPosition;

layout(location=0) in vec4 aPosition;
layout(location=1) in vec2 aTexCoord;

uniform mat4 uPMatrix;
uniform mat4 uMMatrix;
uniform mat4 uVMatrix;

vec2 touchPos = vec2(0.0);

void main() {
    vec4 pos = aPosition;

    gl_Position = uPMatrix * uVMatrix * uMMatrix * pos;
    vTexCoord = aTexCoord;
    vPosition = aPosition.xy;
}
