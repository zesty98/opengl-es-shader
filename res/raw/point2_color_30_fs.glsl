#version 300 es

precision mediump float;

in vec4 vColor;

layout(location=0) out vec4 fragColor;

void main() {
    vec4 color = vColor;
    vec2 distFromCenter = gl_PointCoord - vec2(0.5, 0.5);
    if (length(distFromCenter) > 0.5) {
        color.a = 0.0;
    }
    fragColor = color;
}
