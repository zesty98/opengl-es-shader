precision mediump float;

varying vec2 vTexCoord;
varying vec2 vPosition;

attribute vec4 aPosition;
attribute vec2 aTexCoord;

uniform mat4 uPMatrix;
uniform mat4 uMMatrix;
uniform mat4 uVMatrix;

vec2 touchPos = vec2(0.0);

void main() {
    vec4 pos = aPosition;

    gl_Position = uPMatrix * uVMatrix * uMMatrix * pos;
    vTexCoord = aTexCoord;
    vPosition = aPosition.xy;
}
