precision mediump float;

uniform sampler2D uTexture;

void main() {
    vec4 color = texture2D(uTexture, gl_PointCoord);
//    if (color.a < 0.3) {
//        color.a = 0.0;
//    }
    gl_FragColor = color;
}
