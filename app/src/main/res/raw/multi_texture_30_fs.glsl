#version 300 es

precision mediump float;

in vec2 vTexCoord;

layout(location=0) out vec4 fragColor;

uniform sampler2D uTexture;
uniform sampler2D uTexture2;
uniform int uUseMultiTexture;

void main() {
    vec4 dst = texture(uTexture, vTexCoord);
    vec4 src = texture(uTexture2, vTexCoord);

    vec4 color;

    if (uUseMultiTexture == 1) {
        color = src * src.a + dst * (1.0 - src.a);
    } else {
        color = dst;
    }
    fragColor = color;
}
