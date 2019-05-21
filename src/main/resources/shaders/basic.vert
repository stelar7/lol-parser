#version 330 core

uniform mat4 mvp;

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normals;
layout(location = 2) in vec2 vertexUV;

out vec3 pos;
out vec2 uv;

void main()
{
    gl_Position =  mvp * vec4(position, 1.0);


    // pass on data to the fragment shader
    pos = position;
    uv = vertexUV;
}