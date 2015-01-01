#version 300 es

layout(location=0) in vec4 aPosition;
layout(location=1) in vec2 aTexCoord;

out vec2 vTexCoord;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vTexCoord = aTexCoord;

    gl_Position = pos;
}
