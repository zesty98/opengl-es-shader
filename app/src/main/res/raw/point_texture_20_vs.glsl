attribute vec4 aPosition;
attribute float aSize;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    gl_PointSize = aSize;
    gl_Position = pos;
}
