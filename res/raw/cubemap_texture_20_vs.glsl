varying vec3 vLookupVec;

attribute vec4 aPosition;
attribute vec3 aNormal;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vLookupVec = aPosition.xyz;

    gl_Position = pos;
}
