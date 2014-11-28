varying vec4 vColor;

attribute vec4 aPosition;
attribute vec2 aTexCoord;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp float uPointSize;

uniform sampler2D uTexture;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vColor = texture2D(uTexture, aTexCoord);

    gl_PointSize = uPointSize;
    gl_Position = pos;
}
