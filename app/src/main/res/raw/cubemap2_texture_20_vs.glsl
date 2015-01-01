varying vec3 vLookupVec;

attribute vec4 aPosition;
attribute vec3 aNormal;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;
uniform highp mat3 uNormalMatrix;
uniform highp vec4 uEyePos;

void main() {
    vec4 posES = uVMatrix * uMMatrix * aPosition;
    vec4 pos = uPMatrix * posES;

    vec3 normalES = normalize(uNormalMatrix * aNormal);

    vec4 eyePosES = uVMatrix * uMMatrix * uEyePos;
    vec4 eyeDir = posES - eyePosES;
    vLookupVec = reflect(eyeDir.xyz, normalES);

    gl_Position = pos;
}
