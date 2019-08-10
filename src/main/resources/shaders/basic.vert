#version 330 core

uniform mat4 mvp;
uniform mat4 bones[255];

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normals;
layout(location = 2) in vec2 vertexUV;

layout(location = 3) in vec4 boneIndecies;
layout(location = 4) in vec4 boneWeights;

out vec2 uv;

void main()
{
    mat4 BoneTransform = mat4(1);
    BoneTransform += bones[int(boneIndecies[0])] * boneWeights[0];
    BoneTransform += bones[int(boneIndecies[1])] * boneWeights[1];
    BoneTransform += bones[int(boneIndecies[2])] * boneWeights[2];
    BoneTransform += bones[int(boneIndecies[3])] * boneWeights[3];


    gl_Position =  mvp * BoneTransform * vec4(position, 1.0);
    uv = vertexUV;
}