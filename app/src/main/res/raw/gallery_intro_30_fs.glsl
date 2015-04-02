precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform vec2 uPointSizeInTexCoord;

void main() {
    vec2 halfTCFactor = uPointSizeInTexCoord * 0.5;
    vec2 texCoord = vTexCoord - halfTCFactor + gl_PointCoord * uPointSizeInTexCoord;
    vec4 color = texture2D(uTexture, texCoord);

    if (color.r > 0.9) {
        color.a = 0.0;
    }
    gl_FragColor = color;
}
