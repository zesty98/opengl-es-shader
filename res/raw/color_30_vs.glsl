#version 300 es

layout(location=0) in vec4 aPosition;
layout(location=3) in vec4 aColor;

out vec4 vColor;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vColor = aColor;

    gl_Position = pos;
}
