varying vec2 vTexCoord;
varying float vNormalizedDistFromTouch;

attribute vec4 aPosition;
attribute vec2 aTexCoord;
attribute float aNormalizedDistFromTouch;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp float uPointSize;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vTexCoord = aTexCoord;
    vNormalizedDistFromTouch = aNormalizedDistFromTouch;

    gl_PointSize = uPointSize;
    gl_Position = pos;
}
