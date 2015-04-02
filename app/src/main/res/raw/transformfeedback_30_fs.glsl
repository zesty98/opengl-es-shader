#version 300 es

precision mediump float;

in vec4 vColor;

layout(location=0) out vec4 fragColor;

void main() {
    vec4 color = vColor;

    fragColor = color;
}