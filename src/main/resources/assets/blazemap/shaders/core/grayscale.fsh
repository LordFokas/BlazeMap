#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;

out vec4 fragColor;

const vec3 lum = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 color = texture(Sampler0, texCoord0) * ColorModulator;
    if (color.a == 0.0) { discard; }

    fragColor = vec4(vec3(dot(color.rgb, lum)), color.a);
}