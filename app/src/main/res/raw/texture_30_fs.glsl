#version 300 es

precision mediump float;

in vec2 vTexCoord;

layout(location=0) out vec4 fragColor;

uniform sampler2D uTexture;

void main() {
    fragColor = texture(uTexture, vTexCoord);
}
