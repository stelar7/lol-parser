#version 150 core

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

in vec3 position;

void main()
{
    gl_Position =
    model *
    view *
    projection *
    vec4(position, 1.0);
}