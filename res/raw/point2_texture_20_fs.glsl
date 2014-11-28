precision mediump float;

varying vec2 vTexCoord;
varying float vNormalizedDistFromTouch;

uniform sampler2D uTexture;
uniform highp vec2 uPointSizeInTexCoord;

const float MAX_DIST_FROM_CENTER = 0.7071;

void main() {
    vec2 halfTCFactor = uPointSizeInTexCoord * 0.5;
    vec2 texCoord = vTexCoord - halfTCFactor + gl_PointCoord * uPointSizeInTexCoord;
    vec4 color = texture2D(uTexture, texCoord);

    // [0.0, 1.0] -> [0.5, MAX_DIST_FROM_CENTER]
    float distOffset = vNormalizedDistFromTouch * (MAX_DIST_FROM_CENTER - 0.5) + 0.5;
    vec2 distFromCenter = gl_PointCoord - vec2(0.5, 0.5);

    if (length(distFromCenter) > distOffset) {
        color.a = 0.0;
    }
    gl_FragColor = color;
}
