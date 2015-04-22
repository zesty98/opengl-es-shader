precision mediump float;

varying vec2 vTexCoord;

uniform sampler2D uTexture;
uniform sampler2D uTexture2;
uniform int uUseMultiTexture;

void main() {
    vec4 dst = texture2D(uTexture, vTexCoord);
    float dstAlpha = dst.a;
    vec4 src = texture2D(uTexture2, vTexCoord);
    float srcAlpha = src.a;

    vec4 color;

    if (uUseMultiTexture == 1) {
        color = src * srcAlpha + dst * (1.0 - srcAlpha);
    } else {
        color = dst;
    }
    gl_FragColor = color;
}
