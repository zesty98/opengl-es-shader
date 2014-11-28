#version 300 es

precision mediump float;

in vec2 vTexCoord;
in vec2 vPosition;

layout(location=0) out vec4 fragColor;

uniform sampler2D uTexture;
uniform float uAlpha;

uniform vec2 uTouchPosInFS;
uniform vec2 uTouchPosInVS;
uniform float uRadius;
uniform float uBandWidth;

vec4 color = vec4(0.0, 0.0, 0.0, 1.0);

void main() {
    vec2 texCoord = vTexCoord;

    highp vec2
    delta = vec2(vPosition - uTouchPosInVS);
    float distFromTouchPos = length(delta);
    float dist = distFromTouchPos - uRadius;

    if (dist < uBandWidth && uRadius > 0.0) {
        vec2 dir = normalize(delta);
        dir = vec2(dir.x, -dir.y);
        float normalDistFromMax = (uBandWidth - dist) / uBandWidth;
        normalDistFromMax = pow(normalDistFromMax, 2.0);
        delta = 0.1 * normalDistFromMax * dir;
        texCoord += delta;

        color = vec4(vec3(1.0) * normalDistFromMax, 1.0);

        fragColor = texture(uTexture, texCoord) + color;
    } else {
        fragColor = texture(uTexture, vTexCoord);
    }

    // for debugging
    if (distFromTouchPos < uRadius && distFromTouchPos > (uRadius - 2.0)) {
        fragColor = vec4(1.0, 0.0, 0.0, 1.0);
    }
}
