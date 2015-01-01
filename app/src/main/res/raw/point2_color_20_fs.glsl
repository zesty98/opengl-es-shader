precision mediump float;

varying vec4 vColor;

void main() {
    vec4 color = vColor;
    vec2 distFromCenter = gl_PointCoord - vec2(0.5, 0.5);
    float length = length(distFromCenter);

    if (length > 0.5) {
        color.a = 0.0;
    }

    gl_FragColor = color;
}
