precision mediump float;

varying vec2 vTexCoord;

uniform sampler2D uTexture;
uniform sampler2D uTexture2;
uniform int uUseMultiTexture;

void main() {
    vec4 dst = texture2D(uTexture, vTexCoord);
    vec4 src = texture2D(uTexture2, vTexCoord);
    vec4 color;

    if (uUseMultiTexture == 1) {
        color = src * src.a + dst * (1.0 - src.a);
    } else {
        color = dst;
    }
    gl_FragColor = color;
}
