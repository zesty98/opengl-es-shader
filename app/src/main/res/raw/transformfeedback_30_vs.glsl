#version 300 es

layout(location=0) in vec4 aPosition;
layout(location=1) in vec2 aTexCoord;
layout(location=4) in float aVelocityFactor;
layout(location=5) in vec4 aOriginPosition;
layout(location=6) in vec4 aUserData;   // velocity, normalizedDuration, dirX, dirY

out vec4 vPosition;
out vec4 vColor;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp float uPointSize;
uniform vec4 uUserData; // downX, downY, elapsedTime, normalizedTime

uniform sampler2D uTexture;

void main() {
    vec4 pos = aPosition;

    float normalizedTime = uUserData.w;
    float elapsedTime = uUserData.z;
    float particleDuration = aUserData.y;
    elapsedTime *= step(normalizedTime, particleDuration);

    float velocity = aUserData.x;
    vec2 dir = aUserData.zw;
    pos.x = pos.x + dir.x * elapsedTime * velocity;
    pos.y = pos.y + dir.y * elapsedTime * velocity;

    if (normalizedTime == 0.0) {
        pos.xy = aOriginPosition.xy;
    }

    vColor = texture(uTexture, aTexCoord);
    vPosition = pos;

    gl_PointSize = uPointSize;
    gl_Position = uPMatrix * uVMatrix * uMMatrix * aPosition;
}
