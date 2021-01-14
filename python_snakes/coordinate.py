class Coordinate:
    x = 0
    y = 0
    
    def __init__(self, newx, newy):
        self.x = newx
        self.y = newy
    
    def __eq__(self, other): 
        if not isinstance(other, Coordinate):
            # don't attempt to compare against unrelated types
            return False
        if self.x == other.x and self.y == other.y:
            return True
        return False
    
    # Check whether coordinate is in bounds of board
    # Return True if this coordinate is in bounds
    def in_bounds(self, maze_width, maze_height):
        if x >= 0 and y >= 0 and x < maze_width and y < maze_height:
            return True
        else:
            return False

    # Move coordinate in direction
    # Return a moved coordinate
    def move_to(self, direction):
        c = Coordinate(self.x, self.y)
        if direction == Direction.UP:
            c.y = c.y + 1 
        elif direction == Direction.DOWN:
            c.y = c.y - 1
        elif direction == Direction.LEFT:
            c.x = c.x - 1
        elif direction == Direction.RIGHT:
            c.x = c.x + 1
        return c
