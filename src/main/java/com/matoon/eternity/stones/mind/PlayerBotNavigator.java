package com.matoon.eternity.stones.mind;

import net.minecraft.block.BlockLadder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A small A* path-finder walking a mind-controlled player, who has no navigator of their own: at most 384 nodes within
 * 20 blocks, planned again at most once a second.
 */
final class PlayerBotNavigator {

    private static final int SEARCH_LIMIT = 384, RANGE = 20;
    private static final Comparator<Node> BY_SCORE = Comparator.comparingDouble(node -> node.f);

    private List<BlockPos> path = Collections.emptyList();
    private int index;
    private BlockPos goal;
    private long nextRepath;

    BlockPos next(EntityPlayerMP player, BlockPos requested, long tick) {
        BlockPos start = feet(player);
        if (goal == null || goal.distanceSq(requested) > 4 || tick >= nextRepath || index >= path.size()) {
            goal = closestWalkable(player.world, requested);
            path = find(player.world, start, goal);
            index = path.size() > 1 ? 1 : 0;
            nextRepath = tick + 20;
        }
        while (index < path.size() && distanceTo(player, path.get(index)) < 0.42D) {
            index++;
        }
        return index < path.size() ? path.get(index) : requested;
    }

    private static BlockPos feet(EntityPlayerMP player) {
        return new BlockPos(MathHelper.floor(player.posX), MathHelper.floor(player.getEntityBoundingBox().minY), MathHelper.floor(player.posZ));
    }

    private static double distanceTo(EntityPlayerMP player, BlockPos pos) {
        double x = player.posX - (pos.getX() + 0.5D), z = player.posZ - (pos.getZ() + 0.5D);
        return Math.sqrt(x * x + z * z);
    }

    private static BlockPos closestWalkable(World world, BlockPos pos) {
        for (int dy = 2; dy >= -3; dy--) {
            if (walkable(world, pos.up(dy))) {
                return pos.up(dy);
            }
        }
        return pos;
    }

    private static List<BlockPos> find(World world, BlockPos start, BlockPos end) {
        if (!walkable(world, start)) {
            start = closestWalkable(world, start);
        }
        if (!walkable(world, end)) {
            return Collections.emptyList();
        }
        PriorityQueue<Node> open = new PriorityQueue<>(64, BY_SCORE);
        Map<BlockPos, Node> known = new HashMap<>(512);
        Set<BlockPos> closed = new HashSet<>(512);
        Node first = new Node(start, null, 0, heuristic(start, end));
        open.add(first);
        known.put(start, first);
        Search search = new Search(world, start, end, open, known, closed);
        int searched = 0;
        while (!open.isEmpty() && searched++ < SEARCH_LIMIT) {
            Node node = open.poll();
            if (!closed.add(node.pos)) {
                continue;
            }
            if (node.pos.distanceSq(end) <= 1) {
                return build(node);
            }
            search.step(node, 1, 0);
            search.step(node, -1, 0);
            search.step(node, 0, 1);
            search.step(node, 0, -1);
            if (ladder(world, node.pos)) {
                if (walkable(world, node.pos.up())) {
                    search.add(node, node.pos.up());
                }
                if (walkable(world, node.pos.down())) {
                    search.add(node, node.pos.down());
                }
            }
        }
        return Collections.emptyList();
    }

    private static final class Search {
        final World world;
        final BlockPos start, end;
        final PriorityQueue<Node> open;
        final Map<BlockPos, Node> known;
        final Set<BlockPos> closed;

        Search(World world, BlockPos start, BlockPos end, PriorityQueue<Node> open, Map<BlockPos, Node> known, Set<BlockPos> closed) {
            this.world = world;
            this.start = start;
            this.end = end;
            this.open = open;
            this.known = known;
            this.closed = closed;
        }

        /** A step to the side, up one, level, or down one, whichever stands. */
        void step(Node node, int dx, int dz) {
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos candidate = node.pos.add(dx, dy, dz);
                if (walkable(world, candidate)) {
                    add(node, candidate);
                    return;
                }
            }
        }

        void add(Node node, BlockPos candidate) {
            if (closed.contains(candidate) || Math.abs(candidate.getX() - start.getX()) > RANGE || Math.abs(candidate.getZ() - start.getZ()) > RANGE) {
                return;
            }
            double g = node.g + (candidate.getY() == node.pos.getY() ? 1D : 1.4D);
            Node old = known.get(candidate);
            if (old == null || g < old.g) {
                Node next = new Node(candidate, node, g, g + heuristic(candidate, end));
                known.put(candidate, next);
                open.add(next);
            }
        }
    }

    private static boolean walkable(World world, BlockPos pos) {
        return world.isBlockLoaded(pos) && clear(world, pos) && clear(world, pos.up()) && (solid(world, pos.down()) || ladder(world, pos));
    }

    private static boolean clear(World world, BlockPos pos) {
        return world.isAirBlock(pos) || ladder(world, pos);
    }

    private static boolean solid(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial().blocksMovement();
    }

    private static boolean ladder(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof BlockLadder;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) * 1.4D + Math.abs(a.getZ() - b.getZ());
    }

    private static List<BlockPos> build(Node node) {
        LinkedList<BlockPos> out = new LinkedList<>();
        for (; node != null; node = node.parent) {
            out.addFirst(node.pos);
        }
        return out;
    }

    private static final class Node {
        final BlockPos pos;
        final Node parent;
        final double g, f;

        Node(BlockPos pos, Node parent, double g, double f) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }
    }
}
