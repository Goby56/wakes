#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec3 rgb = hsv2rgb(vertexColor.xyz);
    vec4 color = texture(Sampler0, texCoord0) * vec4(rgb, vertexColor.w);
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color * ColorModulator;
}