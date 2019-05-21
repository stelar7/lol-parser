#version 330 core

out vec4 outColor;

in vec3 pos;
in vec2 uv;

uniform sampler2D texImg;

void main()
{
    //outColor = max(vec4(pos, 1.0), vec4(0.1));
    outColor = texture(texImg, uv);
}