#version 300 es

precision mediump float;

layout(location=0) out vec4 fragColor;

uniform sampler2D uTexture;

void main() {
    vec2 coord = gl_PointCoord;
    fragColor = texture(uTexture, coord);
}
